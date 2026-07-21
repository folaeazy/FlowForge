package com.flowforge.api.dto;

public record Tenant(
        long capacity,
        long ratePerSec)
{ }
