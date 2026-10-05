package dev.takuma.event_hub.repository;

import dev.takuma.event_hub.entity.Ticket;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	Optional<Ticket> findByCode(String code);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select ticket from Ticket ticket where ticket.code = :code")
	Optional<Ticket> findByCodeForUpdate(@Param("code") String code);

	List<Ticket> findByOrderId(Long orderId);

	@Query("""
			select ticket from Ticket ticket
			join ticket.order buyerOrder
			join ticket.ticketType ticketType
			join ticketType.event event
			where buyerOrder.buyerEmail = :email or event.seller.email = :email
			order by ticket.id desc
			""")
	List<Ticket> findVisibleTo(@Param("email") String email);

}
