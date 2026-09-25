package io.d4tzz.ecommerce.application.cart;

import io.d4tzz.ecommerce.domain.cart.CartRepository;
import io.d4tzz.ecommerce.domain.user.UserId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCartUseCase {

    public record Query(Long userId) {}

    private final CartRepository cartRepository;
    private final CartDtoAssembler cartDtoAssembler;

    public GetCartUseCase(CartRepository cartRepository, CartDtoAssembler cartDtoAssembler) {
        this.cartRepository = cartRepository;
        this.cartDtoAssembler = cartDtoAssembler;
    }

    public CartDto execute(Query query) {
        UserId userId = UserId.of(query.userId());
        return cartRepository
            .findByUserId(userId)
            .map(cartDtoAssembler::toDto)
            .orElseGet(() -> CartDto.empty(query.userId()));
    }
}
