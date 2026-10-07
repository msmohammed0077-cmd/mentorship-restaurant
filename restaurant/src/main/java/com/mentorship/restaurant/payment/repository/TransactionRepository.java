package com.mentorship.restaurant.payment.repository;

import com.mentorship.restaurant.payment.model.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {}
