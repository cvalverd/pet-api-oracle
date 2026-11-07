package com.example.petapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.petapi.exception.PetNotFoundException;
import com.example.petapi.model.Pet;
import com.example.petapi.service.PetService;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebMvcTest(PetController.class)
class PetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PetService service;

    @Autowired
    private ObjectMapper mapper;

    private Pet pet;

    @BeforeEach
    void setUp() {
        pet = new Pet(1L, "Firulais", "Labrador", 3);
    }

    @Test
    void testGetAllPets() throws Exception {
        when(service.getAllPets()).thenReturn(List.of(pet));
        mockMvc.perform(get("/pets"))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(List.of(pet))));
    }

    @Test
    void testGetPetById() throws Exception {
        when(service.getPetById(1L)).thenReturn(pet);
        mockMvc.perform(get("/pets/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Firulais"))
                .andExpect(jsonPath("$.breed").value("Labrador"))
                .andExpect(jsonPath("$.age").value(3));
    }

    @Test
    void testGetPetByIdNotFound() throws Exception {
        when(service.getPetById(99L)).thenThrow(new PetNotFoundException(99L));
        mockMvc.perform(get("/pets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("No existe la mascota con id 99"));
    }

    @Test
    void testCreatePet() throws Exception {
        when(service.createPet(any(Pet.class))).thenReturn(pet);
        mockMvc.perform(post("/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(pet)))
                .andExpect(status().isCreated())
                .andExpect(content().json(mapper.writeValueAsString(pet)));
    }

    @Test
    void testCreatePetInvalido() throws Exception {
        Pet invalido = new Pet(null, "", "Labrador", -1);
        mockMvc.perform(post("/pets")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(invalido)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.age").exists());
    }

    @Test
    void testUpdatePet() throws Exception {
        when(service.updatePet(eq(1L), any(Pet.class))).thenReturn(pet);
        mockMvc.perform(put("/pets/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(pet)))
                .andExpect(status().isOk())
                .andExpect(content().json(mapper.writeValueAsString(pet)));
    }

    @Test
    void testDeletePet() throws Exception {
        mockMvc.perform(delete("/pets/1"))
                .andExpect(status().isNoContent());
        verify(service).deletePet(1L);
    }

    @Test
    void testDeletePetNotFound() throws Exception {
        doThrow(new PetNotFoundException(5L)).when(service).deletePet(5L);
        mockMvc.perform(delete("/pets/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testErrorInesperado() throws Exception {
        when(service.getAllPets()).thenThrow(new IllegalStateException("falla"));
        mockMvc.perform(get("/pets"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Ocurrio un error inesperado"));
    }

    @Test
    void testCorsPermiteAngular() throws Exception {
        mockMvc.perform(options("/pets")
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:4200"));
    }
}
