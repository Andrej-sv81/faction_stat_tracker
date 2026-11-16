package com.dnd.simulator.entity;
import jakarta.persistence.*; import java.time.Instant;
@Entity @Table(name="world_events") public class WorldEvent {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id; @Column(nullable=false) private String title; private String type; @Column(length=2000) private String details; private Instant occurredAt=Instant.now();
 @ManyToOne(optional=false) private World world;
 public WorldEvent(){} public WorldEvent(String t,String type,String d,World w){title=t;this.type=type;details=d;world=w;}
 public String getId(){return id;} public String getTitle(){return title;} public void setTitle(String v){title=v;} public String getType(){return type;} public void setType(String v){type=v;} public String getDetails(){return details;} public void setDetails(String v){details=v;} public Instant getOccurredAt(){return occurredAt;} public World getWorld(){return world;}
}
