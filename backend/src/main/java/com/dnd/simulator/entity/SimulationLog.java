package com.dnd.simulator.entity;
import jakarta.persistence.*; import java.time.Instant; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity public class SimulationLog {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id; private Instant createdAt=Instant.now(); @Column(length=3000) private String message;
 @ManyToOne(optional=false) private World world;
 public SimulationLog(){} public SimulationLog(String m,World w){message=m;world=w;} public String getId(){return id;} public Instant getCreatedAt(){return createdAt;} public String getMessage(){return message;} @JsonIgnore public World getWorld(){return world;}
}
