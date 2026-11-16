package com.dnd.simulator.entity;
import jakarta.persistence.*; import java.time.Instant; import java.util.*;
@Entity public class World {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id;
 @Column(nullable=false) private String name; private String description; private Instant createdAt=Instant.now();
 @ManyToOne(optional=false) private AppUser owner;
 public World(){} public World(String name,String description,AppUser owner){this.name=name;this.description=description;this.owner=owner;}
 public String getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} public AppUser getOwner(){return owner;} public Instant getCreatedAt(){return createdAt;}
}
