package io.d4tzz.ecommerce.order.domain;

/*

confirm                 ship                    deliver
┌─────────┐ ─────────► ┌───────────┐ ──────► ┌──────────┐ ─────────► ┌───────────┐
│ PENDING │            │ CONFIRMED │         │ SHIPPING │            │ DELIVERED │
└─────────┘            └───────────┘         └──────────┘            └───────────┘
│                       │                     │
│ cancel (khách/admin)  │ cancel (admin)      │ return (giao thất bại)
│ expire (job)          │                     ▼
▼                       ▼                    ┌──────────┐
┌───────────────────────────────┐            │ RETURNED │
│           CANCELLED           │            └──────────┘
└───────────────────────────────┘

*/

public enum OrderStatus {
    PENDING,
    CONFIRMED,
    SHIPPING,
    DELIVERED,
    RETURNED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == SHIPPING || next == CANCELLED;
            case SHIPPING -> next == DELIVERED || next == RETURNED;
            case DELIVERED, RETURNED, CANCELLED -> false;
        };
    }

    public boolean isFinal() {
        return this == DELIVERED || this == RETURNED || this == CANCELLED;
    }
}
