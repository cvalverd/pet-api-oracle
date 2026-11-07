package com.example.petapi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.petapi.exception.PetNotFoundException;
import com.example.petapi.model.Pet;
import com.example.petapi.repository.PetRepository;

@ExtendWith(MockitoExtension.class)
class PetServiceImplTest {

    @Mock
    private PetRepository repository;

    @InjectMocks
    private PetServiceImpl service;

    private Pet pet;

    @BeforeEach
    void setUp() {
        pet = new Pet(1L, "Firulais", "Labrador", 3);
    }

    @Test
    void testGetAllPets() {
        when(repository.findAll()).thenReturn(List.of(pet));
        assertEquals(List.of(pet), service.getAllPets());
    }

    @Test
    void testGetPetById() {
        when(repository.findById(1L)).thenReturn(Optional.of(pet));
        assertEquals(pet, service.getPetById(1L));
    }

    @Test
    void testGetPetByIdNotFound() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThrows(PetNotFoundException.class, () -> service.getPetById(9L));
    }

    @Test
    void testCreatePetIgnoraElId() {
        when(repository.save(pet)).thenReturn(pet);
        service.createPet(pet);
        assertNull(pet.getId());
        verify(repository).save(pet);
    }

    @Test
    void testUpdatePetExists() {
        when(repository.existsById(1L)).thenReturn(true);
        when(repository.save(pet)).thenReturn(pet);
        Pet result = service.updatePet(1L, pet);
        assertEquals(1L, pet.getId());
        assertEquals(pet, result);
    }

    @Test
    void testUpdatePetNotExists() {
        when(repository.existsById(1L)).thenReturn(false);
        assertThrows(PetNotFoundException.class, () -> service.updatePet(1L, pet));
        verify(repository, never()).save(any());
    }

    @Test
    void testDeletePet() {
        when(repository.existsById(1L)).thenReturn(true);
        service.deletePet(1L);
        verify(repository).deleteById(1L);
    }

    @Test
    void testDeletePetNotExists() {
        when(repository.existsById(1L)).thenReturn(false);
        assertThrows(PetNotFoundException.class, () -> service.deletePet(1L));
        verify(repository, never()).deleteById(any());
    }
}
