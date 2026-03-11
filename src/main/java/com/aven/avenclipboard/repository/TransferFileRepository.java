package com.aven.avenclipboard.repository;

import com.aven.avenclipboard.model.TransferFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransferFileRepository extends JpaRepository<TransferFile, Long> {
}
