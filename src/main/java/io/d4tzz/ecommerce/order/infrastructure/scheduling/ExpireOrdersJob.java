package io.d4tzz.ecommerce.order.infrastructure.scheduling;

import io.d4tzz.ecommerce.order.application.ExpireOrdersUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs {@link ExpireOrdersUseCase} every minute, so a prepaid order is cancelled and its stock released at most
 * about a minute after its time to pay is over. Whether a late payment is accepted does not depend on this job:
 * the order checks its own deadline.
 *
 * <p>Safe if several app instances run it at once: each order is locked and expired only once.
 */
@Component
public class ExpireOrdersJob {

    private static final Logger log = LoggerFactory.getLogger(ExpireOrdersJob.class);

    private final ExpireOrdersUseCase expireOrdersUseCase;

    public ExpireOrdersJob(ExpireOrdersUseCase expireOrdersUseCase) {
        this.expireOrdersUseCase = expireOrdersUseCase;
    }

    // fixedDelay: the next run starts a minute after the previous one ends, so runs never overlap
    @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
    public void run() {
        int expired = expireOrdersUseCase.execute();
        if (expired > 0) {
            log.info("Cancelled {} orders whose time to pay was over", expired);
        }
    }
}
