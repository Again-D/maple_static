package com.maple.growth.controller;

import com.maple.growth.dto.api.ApiResponse;
import com.maple.growth.dto.api.CharacterLookupResponseDto;
import com.maple.growth.dto.api.DashboardResponseDto;
import com.maple.growth.dto.api.EventsResponseDto;
import com.maple.growth.dto.api.GrowthHistoryDto;
import com.maple.growth.dto.api.RefreshResponseDto;
import com.maple.growth.service.CharacterLookupService;
import com.maple.growth.service.KstClock;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/v1/characters")
public class CharacterController {

    private final CharacterLookupService characterLookupService;
    private final KstClock kstClock;

    @GetMapping("/{name}")
    public ApiResponse<CharacterLookupResponseDto> getCharacter(@PathVariable String name) {
        return ApiResponse.success(characterLookupService.lookupOrRegister(name), kstClock.now(), kstClock.zoneId().getId());
    }

    @GetMapping("/{name}/dashboard")
    public ApiResponse<DashboardResponseDto> getDashboard(@PathVariable String name) {
        return ApiResponse.success(characterLookupService.dashboard(name), kstClock.now(), kstClock.zoneId().getId());
    }

    @GetMapping("/{name}/growth-history")
    public ApiResponse<GrowthHistoryDto> getGrowthHistory(
            @PathVariable String name,
            @RequestParam(defaultValue = "7d") String range
    ) {
        if (!"7d".equals(range)) {
            throw new com.maple.growth.service.ValidationException("현재는 7d 범위만 지원합니다.");
        }
        var character = characterLookupService.requireExisting(name);
        return ApiResponse.success(characterLookupService.growthHistory(character, 7), kstClock.now(), kstClock.zoneId().getId());
    }

    @GetMapping("/{name}/events")
    public ApiResponse<EventsResponseDto> getEvents(
            @PathVariable String name,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit
    ) {
        var character = characterLookupService.requireExisting(name);
        return ApiResponse.success(characterLookupService.events(character, limit), kstClock.now(), kstClock.zoneId().getId());
    }

    @PostMapping("/{name}/refresh")
    public ApiResponse<RefreshResponseDto> refresh(@PathVariable String name) {
        return ApiResponse.success(characterLookupService.refresh(name), kstClock.now(), kstClock.zoneId().getId());
    }
}
