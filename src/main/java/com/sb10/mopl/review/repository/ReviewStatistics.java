package com.sb10.mopl.review.repository;

public record ReviewStatistics(long reviewCount, double averageRating) {

  public static ReviewStatistics empty() {
    return new ReviewStatistics(0L, 0.0);
  }
}
