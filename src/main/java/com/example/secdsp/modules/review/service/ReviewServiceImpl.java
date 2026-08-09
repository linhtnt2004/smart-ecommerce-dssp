package com.example.secdsp.modules.review.service;

import com.example.secdsp.common.exception.*;
import com.example.secdsp.common.util.SecurityUtils;
import com.example.secdsp.modules.order.entity.OrderStatus;
import com.example.secdsp.modules.order.repository.OrderItemRepository;
import com.example.secdsp.modules.product.entity.Product;
import com.example.secdsp.modules.product.repository.ProductRepository;
import com.example.secdsp.modules.review.dto.request.CreateReviewRequest;
import com.example.secdsp.modules.review.dto.request.UpdateReviewRequest;
import com.example.secdsp.modules.review.dto.response.RatingSummaryResponse;
import com.example.secdsp.modules.review.dto.response.ReviewResponse;
import com.example.secdsp.modules.review.entity.ProductReview;
import com.example.secdsp.modules.review.repository.ProductReviewRepository;
import com.example.secdsp.modules.user.entity.User;
import com.example.secdsp.modules.user.entity.UserRole;
import com.example.secdsp.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Override
    public ReviewResponse createReview(
        Long productId,
        CreateReviewRequest request
    ) {

        Long userId = requireCurrentUserId();

        log.info(
            "User {} attempting to create review for product {}",
            userId,
            productId
        );

        Product product = productRepository.findById(productId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Product", productId)
            );

        if (reviewRepository.existsByProduct_IdAndUser_Id(
            productId,
            userId
        )) {

            throw new BusinessException(
                ErrorCode.RESOURCE_ALREADY_EXISTS,
                "You already reviewed this product."
            );
        }

        boolean purchased = orderItemRepository
            .existsByOrder_User_IdAndProduct_IdAndOrder_Status(
                userId,
                productId,
                OrderStatus.DELIVERED
            );

        if (!purchased) {
            throw new BusinessException(
                ErrorCode.INVALID_REQUEST,
                "You can only review products you purchased."
            );
        }

        User user = userRepository.getReferenceById(userId);

        ProductReview review = new ProductReview();
        review.setProduct(product);
        review.setUser(user);
        review.setRating(request.rating());
        review.setComment(request.comment());

        ProductReview savedReview = reviewRepository.save(review);

        log.info(
            "Review {} created successfully for product {} by user {}",
            savedReview.getId(),
            productId,
            userId
        );

        return mapToResponse(savedReview);
    }

    @Override
    public ReviewResponse updateReview(
        Long reviewId,
        UpdateReviewRequest request
    ) {

        Long currentUserId = requireCurrentUserId();

        log.info(
            "User {} attempting to update review {}",
            currentUserId,
            reviewId
        );

        ProductReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Review", reviewId)
            );

        if (!review.getUser().getId().equals(currentUserId)) {
            throw new ForbiddenException(
                "You cannot update this review."
            );
        }

        review.setRating(request.rating());
        review.setComment(request.comment());

        log.info(
            "Review {} updated successfully by user {}",
            reviewId,
            currentUserId
        );

        return mapToResponse(review);
    }

    @Override
    public void deleteReview(Long reviewId) {

        Long currentUserId = requireCurrentUserId();

        log.info(
            "User {} attempting to delete review {}",
            currentUserId,
            reviewId
        );

        ProductReview review = reviewRepository.findById(reviewId)
            .orElseThrow(() ->
                             new ResourceNotFoundException("Review", reviewId)
            );

        boolean isOwner =
            review.getUser().getId().equals(currentUserId);

        boolean isAdmin =
            SecurityUtils.hasRole(UserRole.ADMIN);

        if (!isOwner && !isAdmin) {
            throw new ForbiddenException(
                "You cannot delete this review."
            );
        }

        reviewRepository.delete(review);

        log.info(
            "Review {} deleted successfully by user {}",
            reviewId,
            currentUserId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewResponse> getReviews(
        Long productId,
        Pageable pageable
    ) {

        log.debug(
            "Fetching reviews for product {} with pageable {}",
            productId,
            pageable
        );

        return reviewRepository
            .findByProduct_Id(productId, pageable)
            .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public RatingSummaryResponse getRatingSummary(Long productId) {

        log.debug(
            "Fetching rating summary for product {}",
            productId
        );

        Object[] result =
            reviewRepository.getRatingSummary(productId);

        Number avgValue = result[0] instanceof Number
            ? (Number) result[0]
            : null;

        Number countValue = result[1] instanceof Number
            ? (Number) result[1]
            : null;

        double averageRating =
            avgValue != null
                ? avgValue.doubleValue()
                : 0.0;

        long reviewCount =
            countValue != null
                ? countValue.longValue()
                : 0L;

        return new RatingSummaryResponse(
            averageRating,
            reviewCount
        );
    }

    private Long requireCurrentUserId() {

        Long userId = SecurityUtils.getCurrentUserId();

        if (userId == null) {
            throw new UnauthorizedException(
                "Authentication required."
            );
        }

        return userId;
    }

    private ReviewResponse mapToResponse(ProductReview review) {

        return new ReviewResponse(
            review.getId(),
            review.getUser().getId(),
            review.getUser().getFullName(),
            review.getRating(),
            review.getComment(),
            review.getCreatedAt()
        );
    }
}