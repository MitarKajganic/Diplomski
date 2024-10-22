package com.mitar.dipl.service.implementation;

import com.mitar.dipl.exception.custom.BadRequestException;
import com.mitar.dipl.exception.custom.ResourceNotFoundException;
import com.mitar.dipl.mapper.TransactionMapper;
import com.mitar.dipl.model.dto.transaction.TransactionCreateDto;
import com.mitar.dipl.model.dto.transaction.TransactionDto;
import com.mitar.dipl.model.entity.Bill;
import com.mitar.dipl.model.entity.Transaction;
import com.mitar.dipl.repository.BillRepository;
import com.mitar.dipl.repository.TransactionRepository;
import com.mitar.dipl.service.TransactionService;
import com.mitar.dipl.utils.UUIDUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final BillRepository billRepository;
    private final TransactionMapper transactionMapper;

    /**
     * Fetches all transactions.
     *
     * @return List of TransactionDto
     */
    @Override
    public List<TransactionDto> getAllTransactions() {
        log.info("Fetching all transactions.");
        List<TransactionDto> transactionDtos = transactionRepository.findAll().stream()
                .map(transactionMapper::toDto)
                .toList();
        log.info("Fetched {} transactions.", transactionDtos.size());
        return transactionDtos;
    }

    /**
     * Fetches a transaction by its ID.
     *
     * @param transactionId The UUID of the transaction as a string.
     * @return TransactionDto
     */
    @Override
    public TransactionDto getTransactionById(String transactionId) {
        UUID parsedTransactionId = UUIDUtils.parseUUID(transactionId);
        log.debug("Fetching Transaction with ID: {}", parsedTransactionId);

        Transaction transaction = transactionRepository.findById(parsedTransactionId)
                .orElseThrow(() -> {
                    log.warn("Transaction not found with ID: {}", transactionId);
                    return new ResourceNotFoundException("Transaction not found with ID: " + transactionId);
                });

        TransactionDto transactionDto = transactionMapper.toDto(transaction);
        log.info("Retrieved Transaction ID: {}", transactionId);
        return transactionDto;
    }

    /**
     * Fetches transactions by Bill ID.
     *
     * @param billId The UUID of the bill as a string.
     * @return List of TransactionDto
     */
    @Override
    public List<TransactionDto> getTransactionsByBillId(String billId) {
        UUID parsedBillId = UUIDUtils.parseUUID(billId);
        log.debug("Fetching Transactions for Bill ID: {}", parsedBillId);

        List<TransactionDto> transactionDtos = transactionRepository.findAllByBill_Id(parsedBillId).stream()
                .map(transactionMapper::toDto)
                .toList();

        log.info("Fetched {} transactions for Bill ID: {}", transactionDtos.size(), billId);
        return transactionDtos;
    }

    /**
     * Creates a new transaction.
     *
     * @param transactionCreateDto The DTO containing transaction creation data.
     * @return TransactionDto
     */
    @Override
    public TransactionDto createTransaction(TransactionCreateDto transactionCreateDto) {
        UUID billUUID = UUIDUtils.parseUUID(transactionCreateDto.getBillId());
        log.info("Creating transaction for Bill ID: {}", transactionCreateDto.getBillId());

        Bill bill = billRepository.findById(billUUID)
                .orElseThrow(() -> {
                    log.warn("Bill not found with ID: {}", transactionCreateDto.getBillId());
                    return new ResourceNotFoundException("Bill not found with ID: " + transactionCreateDto.getBillId());
                });

        log.debug("Checking if Bill has sufficient funds. Available: {}, Required: {}",
                bill.getFinalAmount(), transactionCreateDto.getAmount());

        if (bill.getFinalAmount().compareTo(transactionCreateDto.getAmount()) < 0) {
            log.warn("Insufficient funds in Bill ID: {}. Available: {}, Required: {}",
                    transactionCreateDto.getBillId(), bill.getFinalAmount(), transactionCreateDto.getAmount());
            throw new BadRequestException("Insufficient funds.");
        }

        bill.calculateFinalAmount();
        billRepository.save(bill);
        log.debug("Deducted {} from Bill ID: {}. New balance: {}",
                transactionCreateDto.getAmount(), transactionCreateDto.getBillId(), bill.getFinalAmount());

        Transaction savedTransaction = transactionRepository.save(transactionMapper.toEntity(transactionCreateDto, bill));
        log.info("Transaction created successfully with ID: {}", savedTransaction.getId());

        return transactionMapper.toDto(savedTransaction);
    }
}
