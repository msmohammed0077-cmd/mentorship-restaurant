package com.mentorship.restaurant.order.repository;

import com.mentorship.restaurant.order.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {}
