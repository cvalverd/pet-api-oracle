package com.example.petapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// Mismos campos que el modelo Pet del frontend Angular: id, name, breed, age.
@Entity
@Table(name = "pet")
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "No puede ingresar un nombre vacio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    @Column(name = "name")
    private String name;

    @NotBlank(message = "No puede ingresar una raza vacia")
    @Size(max = 50, message = "La raza no puede superar los 50 caracteres")
    @Column(name = "breed")
    private String breed;

    @NotNull(message = "Debe indicar la edad")
    @Min(value = 0, message = "La edad no puede ser negativa")
    @Max(value = 40, message = "La edad no puede superar los 40")
    @Column(name = "age")
    private Integer age;

    public Pet() { }

    public Pet(Long id, String name, String breed, Integer age) {
        this.id = id;
        this.name = name;
        this.breed = breed;
        this.age = age;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }

    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
}
