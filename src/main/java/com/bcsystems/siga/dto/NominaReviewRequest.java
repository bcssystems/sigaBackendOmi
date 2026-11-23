package com.bcsystems.siga.dto;
import java.util.List;
public record NominaReviewRequest(List<Long> comisionIds,List<Long> deduccionIds){}
