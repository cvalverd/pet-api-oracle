package com.example.petapi.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PetModelTest {
    @Test
    void testGettersAndSetters() {
        Pet pet = new Pet();
        pet.setId(1L);
        pet.setName("Michi");
        pet.setBreed("Siames");
        pet.setAge(2);
        assertEquals(1L, pet.getId());
        assertEquals("Michi", pet.getName());
        assertEquals("Siames", pet.getBreed());
        assertEquals(2, pet.getAge());
    }
}
