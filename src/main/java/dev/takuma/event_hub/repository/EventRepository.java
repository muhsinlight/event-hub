package dev.takuma.event_hub.repository;

import dev.takuma.event_hub.entity.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, Long> {

	@Query(value = """
			select event from Event event left join event.seller seller
			where event.status <> dev.takuma.event_hub.entity.Event.Status.DRAFT or seller.email = :viewer
			""", countQuery = """
			select count(event) from Event event left join event.seller seller
			where event.status <> dev.takuma.event_hub.entity.Event.Status.DRAFT or seller.email = :viewer
			""")
	Page<Event> findVisible(@Param("viewer") String viewer, Pageable pageable);

	@Query(value = """
			select event from Event event left join event.seller seller
			where event.name = :name
			and (event.status <> dev.takuma.event_hub.entity.Event.Status.DRAFT or seller.email = :viewer)
			""", countQuery = """
			select count(event) from Event event left join event.seller seller
			where event.name = :name
			and (event.status <> dev.takuma.event_hub.entity.Event.Status.DRAFT or seller.email = :viewer)
			""")
	Page<Event> findVisibleByName(@Param("name") String name, @Param("viewer") String viewer, Pageable pageable);

}
