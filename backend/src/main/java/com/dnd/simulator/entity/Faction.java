package com.dnd.simulator.entity;
import jakarta.persistence.*;
@Entity public class Faction {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id; @Column(nullable=false) private String name; private String alignment; private int power=50; private String description;
 @ManyToOne(optional=false) private World world;
 public Faction(){} public Faction(String n,String a,int p,String d,World w){name=n;alignment=a;power=p;description=d;world=w;}
 public String getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getAlignment(){return alignment;} public void setAlignment(String v){alignment=v;} public int getPower(){return power;} public void setPower(int v){power=Math.max(0,Math.min(100,v));} public String getDescription(){return description;} public void setDescription(String v){description=v;} public World getWorld(){return world;}
}
