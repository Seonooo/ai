package personal.ai.core.booking.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import personal.ai.core.booking.domain.model.SeatStatus;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA Repository for Seat
 */
public interface JpaSeatRepository extends JpaRepository<SeatEntity, Long> {

    /**
     * 특정 일정의 예매 가능한 좌석 목록 조회
     */
    @Query("SELECT s FROM SeatEntity s WHERE s.scheduleId = :scheduleId AND s.status = :status")
    List<SeatEntity> findByScheduleIdAndStatus(@Param("scheduleId") Long scheduleId,
                                                @Param("status") SeatStatus status);

    /**
     * 특정 일정의 특정 좌석 번호 조회
     */
    @Query("SELECT s FROM SeatEntity s WHERE s.scheduleId = :scheduleId AND s.seatNumber = :seatNumber")
    Optional<SeatEntity> findByScheduleIdAndSeatNumber(@Param("scheduleId") Long scheduleId,
                                                         @Param("seatNumber") String seatNumber);
}
