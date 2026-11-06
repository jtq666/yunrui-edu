package com.yunrui.edu.points;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import com.yunrui.edu.auth.UserAccessGuard;

@RestController
@RequestMapping("/api/points")
public class PointsController {
    private final PointsService service;
    private final UserAccessGuard accessGuard;

    public PointsController(PointsService service, UserAccessGuard accessGuard) {
        this.service = service;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/events")
    public AddResult add(@Valid @RequestBody AddRequest request) {
        accessGuard.require(request.userId());
        return new AddResult(service.add(
                request.eventId(), request.userId(), request.points(), request.reason()));
    }

    @GetMapping("/boards/{season}")
    public List<PointsBoard.Entry> top(@PathVariable String season,
                                       @RequestParam(defaultValue = "10") int limit) {
        return service.top(season, limit);
    }

    @GetMapping("/boards/{season}/users/{userId}")
    public PointsBoard.Entry mine(@PathVariable String season, @PathVariable long userId) {
        accessGuard.require(userId);
        return service.mine(season, userId);
    }

    public record AddRequest(@NotBlank String eventId,
                             @Positive long userId,
                             @Positive int points,
                             @NotBlank String reason) {
    }

    public record AddResult(boolean accepted) {
    }
}
