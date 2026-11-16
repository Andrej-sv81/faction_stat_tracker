package com.dnd.simulator.service;
import com.dnd.simulator.dto.ApiDtos; import com.dnd.simulator.entity.*; import com.dnd.simulator.repository.*; import org.springframework.stereotype.Service; import org.springframework.transaction.annotation.Transactional; import java.util.*;
@Service public class WorldService {
 private final WorldRepository worlds; private final AppUserRepository users; private final FactionRepository factions; private final WorldEventRepository events; private final SimulationLogRepository logs; private final Random random=new Random();
 public WorldService(WorldRepository w,AppUserRepository u,FactionRepository f,WorldEventRepository e,SimulationLogRepository l){worlds=w;users=u;factions=f;events=e;logs=l;}
 public List<World> list(String email){return worlds.findByOwnerEmailOrderByCreatedAtDesc(email);} public World get(String id,String email){return worlds.findByIdAndOwnerEmail(id,email).orElseThrow(()->new NoSuchElementException("World not found"));}
 public World create(ApiDtos.WorldRequest r,String email){return worlds.save(new World(r.name(),r.description(),users.findByEmail(email).orElseThrow()));}
 public Faction faction(String id,ApiDtos.FactionRequest r,String email){World w=get(id,email);return factions.save(new Faction(r.name(),r.alignment(),r.power(),r.description(),w));}
 public WorldEvent event(String id,ApiDtos.EventRequest r,String email){return events.save(new WorldEvent(r.title(),r.type(),r.details(),get(id,email)));}
 @Transactional public Map<String,Object> simulate(String id,String email){World w=get(id,email);var fs=factions.findByWorldId(id);if(fs.size()>1){var a=fs.get(random.nextInt(fs.size()));a.setPower(a.getPower()+random.nextInt(21)-10);factions.save(a);}var msg="A shifting tide reshaped "+(fs.isEmpty()?"the realm":fs.get(0).getName())+".";logs.save(new SimulationLog(msg,w));return Map.of("message",msg,"factions",fs);}
 public Map<String,Object> data(String id,String email){get(id,email);return Map.of("factions",factions.findByWorldId(id),"events",events.findByWorldIdOrderByOccurredAtDesc(id),"logs",logs.findTop100ByWorldIdOrderByCreatedAtDesc(id));}
}
