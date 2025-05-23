package com.example.backend.controllers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.backend.mappers.BaseMapper;
import com.example.backend.services.BaseServiceInterface;

import jakarta.servlet.http.HttpServletRequest;

public abstract class BaseControllerTest<
    Entity,
    Resource,
    Create,
    Update,
    Repository extends JpaRepository<Entity, Long> & JpaSpecificationExecutor<Entity>, 
    Mapper extends BaseMapper<Entity, Resource, Create, Update>,
    Service extends BaseServiceInterface<Entity, Create, Update>
>{
    @Autowired
    protected MockMvc mockMvc;

    @MockBean
    protected Service service;

    @MockBean
    protected Mapper mapper;

    protected String getExpectedSuccessMessage() {
        return "SUCCESS";
    }

    protected abstract String getApiPath();
    protected abstract String getTestKeyword();
    protected abstract Map<String, String[]> getTestSimpleFilter();
    protected abstract ResultActions getExpectResponseData(ResultActions result, List<Resource> resource) throws Exception;
    protected abstract ResultActions getExpectResponseFilterData(ResultActions result, List<Resource> resource) throws Exception;
    protected abstract List<Entity> createTestEntities();
    protected abstract List<Entity> createTestEntitiesByKeywordFiltered(List<Entity> entities, String keyword);
    protected abstract List<Entity> createTestEntitiesBySimpleFiltered(List<Entity> entities, Map<String, String[]> filters);
    protected abstract List<Resource> createTestResources();
    protected abstract List<Resource> createTestResourcesByKeywordFiltered(List<Resource> resources, String keyword);
    protected abstract List<Resource> createTestResourcesBySimpleFiltered(List<Resource> resources, Map<String, String[]> filters);

    @Test
    void list_NoFilter_ShouldReturnAllRecords() throws Exception {
        List<Entity> mockEntities = createTestEntities(); // Tạo mock dữ liệu entities
        List<Resource> mockResources = createTestResources(); // Tạo mock dữ liệu resources

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String[]>> captor = ArgumentCaptor.forClass(Map.class); // ArgumentCaptor để bắt tham số

        // Stub service và mapper
        when(service.getAll(captor.capture(), any(HttpServletRequest.class))).thenReturn(mockEntities);
        when(mapper.toListResource(mockEntities)).thenReturn(mockResources);

        // Gửi request GET đến API
        mockMvc.perform(get(getApiPath() + "/list")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk()) 
                .andExpect(jsonPath("$.success").value(true)) 
                .andExpect(jsonPath("$.message").value(getExpectedSuccessMessage())) 
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data").isArray()) 
                .andExpect(jsonPath("$.timestamp").exists()) 
                .andExpect(jsonPath("$.errors").doesNotExist()) 
                .andExpect(jsonPath("$.error").doesNotExist()); 

    
        verify(service).getAll(captor.capture(), any(HttpServletRequest.class));
        verify(mapper).toListResource(mockEntities);

        Map<String, String[]> capturedParams = captor.getValue(); 
        assertThat(capturedParams).isNotNull(); 
        assertThat(capturedParams).isEmpty();
    }

    @Test
    void list_withKeywordFilter_ShouldReturnFilteredKeywordRecords() throws Exception {
        // Tạo mock dữ liệu
        List<Entity> mockEntities = createTestEntities();
        List<Resource> mockResources = createTestResources();
        List<Entity> mockFilterEntities = createTestEntitiesByKeywordFiltered(mockEntities, getTestKeyword());
        List<Resource> mockFilterResources = createTestResourcesByKeywordFiltered(mockResources, getTestKeyword());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String[]>> captor = ArgumentCaptor.forClass(Map.class);

        // Stub gọi service và mapper
        when(service.getAll(captor.capture(), any(HttpServletRequest.class))).thenReturn(mockFilterEntities);
        when(mapper.toListResource(mockFilterEntities)).thenReturn(mockFilterResources);

        // Gửi request GET đến API
        ResultActions actions = mockMvc.perform(get(getApiPath() + "/list")
                .param("keyword", getTestKeyword()) // Thêm keyword vào request
                .contentType(MediaType.APPLICATION_JSON));

        // Kiểm tra phản hồi từ API
        getExpectResponseData(actions, mockFilterResources)
                .andExpect(status().isOk()) 
                .andExpect(jsonPath("$.success").value(true)) 
                .andExpect(jsonPath("$.message").value(getExpectedSuccessMessage())) 
                .andExpect(jsonPath("$.status").value("OK")) 
                .andExpect(jsonPath("$.data").isArray()) 
                .andExpect(jsonPath("$.timestamp").exists()) 
                .andExpect(jsonPath("$.errors").doesNotExist()) 
                .andExpect(jsonPath("$.error").doesNotExist()); 


        verify(service).getAll(captor.capture(), any(HttpServletRequest.class)); 
        verify(mapper).toListResource(mockFilterEntities); 

        Map<String, String[]> capturedParams = captor.getValue(); 
        assertThat(capturedParams).isNotNull(); 
        assertThat(capturedParams.get("keyword")).containsExactly(getTestKeyword());
    }

    @Test
    void list_withSimpleFilter_ShouldReturnSimpleFilteredRecords() throws Exception {
        // Mock dữ liệu
        Map<String, String[]> filters = getTestSimpleFilter();
        List<Entity> mockEntities = createTestEntities();
        List<Resource> mockResources = createTestResources();
        List<Entity> mockFilterEntities = createTestEntitiesBySimpleFiltered(mockEntities, filters);
        List<Resource> mockFilterResources = createTestResourcesBySimpleFiltered(mockResources, filters);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String[]>> captor = ArgumentCaptor.forClass(Map.class);

    
        when(service.getAll(captor.capture(), any(HttpServletRequest.class))).thenReturn(mockFilterEntities);
        when(mapper.toListResource(mockFilterEntities)).thenReturn(mockFilterResources);

     
        MockHttpServletRequestBuilder requestBuilder = get(getApiPath() + "/list");
        filters.forEach((key, values) -> {
            for (String value : values) {
                requestBuilder.param(key, value);
            }
        });
        ResultActions actions = mockMvc.perform(requestBuilder.contentType(MediaType.APPLICATION_JSON))
            .andDo(print());

        getExpectResponseFilterData(actions, mockFilterResources)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value(getExpectedSuccessMessage()))
            .andExpect(jsonPath("$.status").value("OK"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.timestamp").exists())
            .andExpect(jsonPath("$.errors").doesNotExist())
            .andExpect(jsonPath("$.error").doesNotExist());

        Map<String, String[]> capturedParams = captor.getValue();
        assertThat(capturedParams).isNotNull();
        filters.forEach((key, values) -> {
            assertThat(capturedParams).containsKey(key);
            assertThat(capturedParams.get(key)).containsExactly(values);
        });

        verify(service).getAll(eq(capturedParams), any(HttpServletRequest.class));
        verify(mapper).toListResource(mockFilterEntities);
    }
}
