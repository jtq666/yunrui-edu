package com.yunrui.edu.trade.domain;

import java.util.Optional;
import java.util.List;

public interface PurchaseOrderRepository {
    void insert(PurchaseOrder order);

    Optional<PurchaseOrder> findByIdForUpdate(long orderId);

    Optional<PurchaseOrder> findById(long orderId);

    List<PurchaseOrder> findByUserId(long userId);

    void save(PurchaseOrder order);
}
