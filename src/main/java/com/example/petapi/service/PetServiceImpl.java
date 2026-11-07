package com.example.petapi.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.petapi.exception.PetNotFoundException;
import com.example.petapi.model.Pet;
import com.example.petapi.repository.PetRepository;

@Service
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;

    public PetServiceImpl(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    public List<Pet> getAllPets() {
        return petRepository.findAll();
    }

    @Override
    public Pet getPetById(Long id) {
        return petRepository.findById(id).orElseThrow(() -> new PetNotFoundException(id));
    }

    @Override
    public Pet createPet(Pet pet) {
        pet.setId(null); // el id lo genera Oracle (IDENTITY)
        return petRepository.save(pet);
    }

    @Override
    public Pet updatePet(Long id, Pet pet) {
        if (!petRepository.existsById(id)) {
            throw new PetNotFoundException(id);
        }
        pet.setId(id);
        return petRepository.save(pet);
    }

    @Override
    public void deletePet(Long id) {
        if (!petRepository.existsById(id)) {
            throw new PetNotFoundException(id);
        }
        petRepository.deleteById(id);
    }
}
