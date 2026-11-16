package com.dnd.simulator.dto;
import jakarta.validation.constraints.*;
public final class AuthDtos {
 public record Credentials(@Email @NotBlank String email,@Size(min=8,max=100) String password){}
 public record Token(String token,String email){}
}
