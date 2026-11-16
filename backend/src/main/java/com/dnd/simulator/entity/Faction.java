package com.dnd.simulator.entity;
import jakarta.persistence.*; import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity public class Faction {
 @Id @GeneratedValue(strategy=GenerationType.UUID) private String id; @Column(nullable=false) private String name; private String alignment; private int power=50; private int wealth=50; private int manpower=50; private int influence=50; private int territory=1; private String imageUrl; private String description;
 @ManyToOne(optional=false) private World world;
 public Faction(){} public Faction(String n,String a,int p,String d,World w){name=n;alignment=a;power=p;description=d;world=w;}
 public String getId(){return id;} public String getName(){return name;} public void setName(String v){name=v;} public String getAlignment(){return alignment;} public void setAlignment(String v){alignment=v;} public int getPower(){return power;} public void setPower(int v){power=clamp(v);} public int getWealth(){return wealth;} public void setWealth(int v){wealth=clamp(v);} public int getManpower(){return manpower;} public void setManpower(int v){manpower=clamp(v);} public int getInfluence(){return influence;} public void setInfluence(int v){influence=clamp(v);} public int getTerritory(){return territory;} public void setTerritory(int v){territory=Math.max(0,v);} public String getImageUrl(){return imageUrl;} public void setImageUrl(String v){imageUrl=v;} public String getDescription(){return description;} public void setDescription(String v){description=v;} @JsonIgnore public World getWorld(){return world;} public void setWorld(World w){world=w;}
 private int clamp(int v){return Math.max(0,Math.min(100,v));}
}
