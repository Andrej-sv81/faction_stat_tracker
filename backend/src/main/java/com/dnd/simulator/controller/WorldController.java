package com.dnd.simulator.controller;
import com.dnd.simulator.dto.ApiDtos; import com.dnd.simulator.service.WorldService; import com.fasterxml.jackson.core.JsonProcessingException; import com.fasterxml.jackson.databind.ObjectMapper; import jakarta.validation.Valid; import org.springframework.http.*; import org.springframework.security.core.Authentication; import org.springframework.web.bind.annotation.*; import java.util.*;
@RestController @RequestMapping("/api/worlds") public class WorldController {
 private final WorldService service; private final ObjectMapper mapper; public WorldController(WorldService s,ObjectMapper m){service=s;mapper=m;} private String user(Authentication a){return a.getName();}
 @GetMapping public List<?> list(Authentication a){return service.list(user(a));}
 @PostMapping public Object create(@Valid @RequestBody ApiDtos.WorldRequest r,Authentication a){return service.create(r,user(a));}
 @GetMapping("/{id}") public Object data(@PathVariable String id,Authentication a){var w=service.get(id,user(a));var d=new HashMap<>(service.data(id,user(a)));d.put("world",w);return d;}
 @PostMapping("/{id}/factions") public Object faction(@PathVariable String id,@Valid @RequestBody ApiDtos.FactionRequest r,Authentication a){return service.faction(id,r,user(a));}
 @PostMapping("/{id}/events") public Object event(@PathVariable String id,@Valid @RequestBody ApiDtos.EventRequest r,Authentication a){return service.event(id,r,user(a));}
 @PostMapping("/{id}/simulate") public Object simulate(@PathVariable String id,Authentication a){return service.simulate(id,user(a));}
 @GetMapping("/{id}/export") public ResponseEntity<byte[]> export(@PathVariable String id,Authentication a)throws JsonProcessingException{String json=mapper.writeValueAsString(service.data(id,user(a)));return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=world-"+id+".json").body(json.getBytes());}
}
