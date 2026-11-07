package com.example.petapi.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.petapi.model.Pet;

public interface PetRepository extends JpaRepository<Pet, Long> {

}
