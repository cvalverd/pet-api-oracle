package com.example.petapi.service;

import java.util.List;

import com.example.petapi.model.Pet;

public interface PetService {
    List<Pet> getAllPets();
    Pet getPetById(Long id);
    Pet createPet(Pet pet);
    Pet updatePet(Long id, Pet pet);
    void deletePet(Long id);
}
