package com.bricopro.util;

import com.bricopro.task.entity.Task;
import com.bricopro.task.entity.Task.TaskStatus;
import com.bricopro.user.entity.ClientProfile;
import com.bricopro.user.entity.User;
import com.bricopro.user.entity.User.Role;
import com.bricopro.user.entity.User.Status;
import com.bricopro.user.entity.WorkerProfile;
import com.bricopro.user.entity.WorkerProfile.ServiceType;
import com.bricopro.payment.entity.Payment;
import com.bricopro.payment.entity.Payment.PaymentMethod;
import com.bricopro.payment.entity.Payment.PaymentStatus;
import com.bricopro.task.entity.Review;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized test fixtures for BricoPro unit tests.
 *
 * Usage:
 *   User client  = TestFixtures.aClient().build();
 *   User worker  = TestFixtures.aWorker().id(5L).email("w@test.ma").build();
 *   Task task    = TestFixtures.aTask(client, worker).agreedPrice(new BigDecimal("300")).build();
 */
public final class TestFixtures {

    private static final AtomicLong ID_SEQ = new AtomicLong(1);

    private TestFixtures() {}

    // ─── ID GENERATOR ─────────────────────────────────────────────────────────

    /** Returns a monotonically increasing unique Long ID for test objects. */
    public static Long nextId() {
        return ID_SEQ.getAndIncrement();
    }

    // ─── USER BUILDERS ────────────────────────────────────────────────────────

    /** Returns a fully configured CLIENT user builder. */
    public static User.UserBuilder aClient() {
        return User.builder()
                .id(nextId())
                .firstName("ClientFirst")
                .lastName("ClientLast")
                .email("client" + nextId() + "@test.ma")
                .phone("+21260" + String.format("%07d", nextId()))
                .passwordHash("$2a$10$hash")
                .role(Role.CLIENT)
                .status(Status.ACTIVE)
                .isVerified(true)
                .isOnline(false)
                .createdAt(LocalDateTime.now().minusDays(30));
    }

    /** Returns a fully configured WORKER user builder. */
    public static User.UserBuilder aWorker() {
        return User.builder()
                .id(nextId())
                .firstName("WorkerFirst")
                .lastName("WorkerLast")
                .email("worker" + nextId() + "@test.ma")
                .phone("+21261" + String.format("%07d", nextId()))
                .passwordHash("$2a$10$hash")
                .role(Role.WORKER)
                .status(Status.ACTIVE)
                .isVerified(true)
                .isOnline(true)
                .createdAt(LocalDateTime.now().minusDays(60));
    }

    /** Returns a fully configured ADMIN user builder. */
    public static User.UserBuilder anAdmin() {
        return User.builder()
                .id(nextId())
                .firstName("Admin")
                .lastName("BricoPro")
                .email("admin@bricopro.ma")
                .passwordHash("$2a$10$hash")
                .role(Role.ADMIN)
                .status(Status.ACTIVE)
                .isVerified(true);
    }

    // ─── WORKER PROFILE BUILDERS ──────────────────────────────────────────────

    /** Returns a worker profile builder pre-loaded with good defaults. */
    public static WorkerProfile.WorkerProfileBuilder aWorkerProfile(User workerUser) {
        return WorkerProfile.builder()
                .id(nextId())
                .user(workerUser)
                .bio("Experienced professional ready to help.")
                .cinVerified(true)
                .averageRating(BigDecimal.valueOf(4.5))
                .totalReviews(20)
                .totalMissions(45)
                .interventionRadiusKm(20)
                .city("Casablanca")
                .latitude(33.589886)
                .longitude(-7.603869)
                .isPremium(false)
                .cancellationCount(0)
                .responseRate(BigDecimal.valueOf(95));
    }

    /** Returns a premium worker profile builder. */
    public static WorkerProfile.WorkerProfileBuilder aPremiumWorkerProfile(User workerUser) {
        return aWorkerProfile(workerUser)
                .isPremium(true)
                .averageRating(BigDecimal.valueOf(4.9))
                .totalReviews(80)
                .totalMissions(150);
    }

    /** Returns a low-rated worker profile builder (close to suspension threshold). */
    public static WorkerProfile.WorkerProfileBuilder aLowRatedWorkerProfile(User workerUser) {
        return aWorkerProfile(workerUser)
                .averageRating(BigDecimal.valueOf(2.8))
                .totalReviews(15)
                .cancellationCount(3)
                .responseRate(BigDecimal.valueOf(40));
    }

    // ─── CLIENT PROFILE BUILDERS ──────────────────────────────────────────────

    /** Returns a client profile builder. */
    public static ClientProfile.ClientProfileBuilder aClientProfile(User clientUser) {
        return ClientProfile.builder()
                .id(nextId())
                .user(clientUser)
                .city("Casablanca")
                .defaultAddress("10 Bd Zerktouni, Casablanca")
                .defaultLatitude(33.5731)
                .defaultLongitude(-7.5898);
    }

    // ─── TASK BUILDERS ────────────────────────────────────────────────────────

    /** Returns a SEARCHING task builder. */
    public static Task.TaskBuilder aTask(User client) {
        return Task.builder()
                .id(nextId())
                .client(client)
                .serviceType(ServiceType.PLUMBING)
                .title("Fix leaking pipe")
                .description("Bathroom pipe is leaking under the sink.")
                .address("Hay Hassani, Casablanca")
                .latitude(33.5442)
                .longitude(-7.6496)
                .scheduledDate(LocalDate.now().plusDays(3))
                .scheduledStart(LocalTime.of(10, 0))
                .scheduledEnd(LocalTime.of(12, 0))
                .budgetMin(BigDecimal.valueOf(150))
                .budgetMax(BigDecimal.valueOf(350))
                .status(TaskStatus.SEARCHING)
                .isUrgent(false)
                .createdAt(LocalDateTime.now());
    }

    /** Returns a CONFIRMED task builder (client + worker assigned). */
    public static Task.TaskBuilder aConfirmedTask(User client, User worker) {
        return aTask(client)
                .worker(worker)
                .status(TaskStatus.CONFIRMED)
                .agreedPrice(BigDecimal.valueOf(250));
    }

    /** Returns a COMPLETED task builder. */
    public static Task.TaskBuilder aCompletedTask(User client, User worker) {
        return aConfirmedTask(client, worker)
                .status(TaskStatus.COMPLETED);
    }

    /** Returns an urgent task builder. */
    public static Task.TaskBuilder anUrgentTask(User client) {
        return aTask(client)
                .isUrgent(true)
                .budgetMin(BigDecimal.valueOf(200))
                .budgetMax(BigDecimal.valueOf(450));
    }

    /** Returns a DISPUTED task builder. */
    public static Task.TaskBuilder aDisputedTask(User client, User worker) {
        return aConfirmedTask(client, worker)
                .status(TaskStatus.DISPUTED)
                .cancellationReason("Worker did not show up on time.");
    }

    // ─── PAYMENT BUILDERS ─────────────────────────────────────────────────────

    /** Returns a completed CASH payment builder. */
    public static Payment.PaymentBuilder aPayment(Task task) {
        BigDecimal gross = task.getAgreedPrice() != null
                ? task.getAgreedPrice() : BigDecimal.valueOf(300);
        BigDecimal fee  = gross.multiply(new BigDecimal("0.12")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal proc = gross.multiply(new BigDecimal("0.015")).setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal net  = gross.subtract(fee).subtract(proc);

        return Payment.builder()
                .id(nextId())
                .task(task)
                .client(task.getClient())
                .worker(task.getWorker())
                .grossAmount(gross)
                .platformFee(fee)
                .processingFee(proc)
                .netAmount(net)
                .currency("MAD")
                .method(PaymentMethod.CASH)
                .status(PaymentStatus.COMPLETED)
                .gatewayReference("BRICO-" + nextId())
                .paidAt(LocalDateTime.now());
    }

    // ─── REVIEW BUILDERS ─────────────────────────────────────────────────────

    /** Returns a 5-star review builder from client → worker. */
    public static Review.ReviewBuilder aReview(Task task) {
        return Review.builder()
                .id(nextId())
                .task(task)
                .reviewer(task.getClient())
                .reviewee(task.getWorker())
                .rating(5)
                .comment("Excellent service, very professional and on time!")
                .createdAt(LocalDateTime.now());
    }

    /** Returns a low-rating (2 star) review builder. */
    public static Review.ReviewBuilder aLowReview(Task task) {
        return aReview(task)
                .rating(2)
                .comment("Did not meet expectations.");
    }

    // ─── CONSTANTS ────────────────────────────────────────────────────────────

    /** A fixed JWT secret suitable for tests (base64, 256-bit). */
    public static final String TEST_JWT_SECRET =
            "dGVzdC1zZWNyZXQta2V5LWZvci1icmljb3Byby10ZXN0aW5nLW9ubHktbm90LXNob3J0";

    /** Casablanca GPS coordinates. */
    public static final double CASABLANCA_LAT = 33.589886;
    public static final double CASABLANCA_LNG = -7.603869;

    /** Rabat GPS coordinates. */
    public static final double RABAT_LAT = 34.020882;
    public static final double RABAT_LNG = -6.84165;

    /** Marrakech GPS coordinates. */
    public static final double MARRAKECH_LAT = 31.6295;
    public static final double MARRAKECH_LNG = -7.9811;

    /** Approximate Casablanca → Rabat distance in km. */
    public static final double CASABLANCA_RABAT_KM = 88.0;
}
