package com.glassstore.order.controller;
import com.glassstore.order.entity.*;
import com.glassstore.order.repository.*;
import com.glassstore.order.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.util.*;
import java.math.BigDecimal;

@RestController @RequiredArgsConstructor
public class PromotionController {
    private final PromotionService promotions;
    private final PosDataService data;
    private final LoyaltyPolicy policy;
    private final LoyaltyAccountRepository accounts;
    private final LoyaltyEntryRepository entries;
    private final VoucherRepository vouchers;
    @ExceptionHandler(ResponseStatusException.class)
    public org.springframework.http.ResponseEntity<Map<String,String>> error(ResponseStatusException error) { return org.springframework.http.ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()==null?"Yêu cầu không hợp lệ.":error.getReason())); }
    @PostMapping("/api/promotions/apply")
    public PromotionService.Quote apply(@RequestBody Order order,@RequestHeader(value="Authorization",required=false) String token) { return promotions.calculate(order,token,false); }
    @GetMapping("/api/loyalty/accounts/{id}")
    public Map<String,Object> account(@PathVariable("id") Long id,@RequestHeader(value="Authorization",required=false) String token) {
        data.actor(token,false); data.customer(token,id);
        return Map.of("customerId",id,"balance",accounts.findById(id).map(LoyaltyAccount::getBalance).orElse(0L),"pointValue",policy.getPointValue(),"maxRedemptionPercent",policy.getMaxRedemptionPercent(),"enabled",policy.isEnabled(),"allowVoucherStack",policy.isAllowVoucherStack());
    }
    public record Adjustment(long points,String reason) {}
    @PostMapping("/api/loyalty/accounts/{id}/adjust") @Transactional
    public LoyaltyAccount adjust(@PathVariable("id") Long id,@RequestBody Adjustment request,@RequestHeader(value="Authorization",required=false) String token) {
        data.actor(token,true); data.customer(token,id);
        if(request.points()==0 || request.reason()==null || request.reason().isBlank() || request.reason().length()>255) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Nhập số điểm và lý do điều chỉnh.");
        LoyaltyAccount account=accounts.lockByCustomerId(id).orElseGet(()->{LoyaltyAccount created=new LoyaltyAccount();created.setCustomerId(id);return accounts.saveAndFlush(created);});
        long balance=Math.addExact(account.getBalance(),request.points()); if(balance<0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Không được để số dư âm.");
        account.setBalance(balance);accounts.save(account);LoyaltyEntry entry=new LoyaltyEntry();entry.setCustomerId(id);entry.setEntryType("ADMIN_ADJUST");entry.setPoints(request.points());entry.setReason(request.reason());entries.save(entry);return account;
    }
    @PostMapping("/api/promotions/vouchers") @Transactional
    public Voucher create(@RequestBody Voucher voucher,@RequestHeader(value="Authorization",required=false) String token) {
        data.actor(token,true);
        if(voucher.getCode()==null || !voucher.getCode().trim().matches("[a-zA-Z0-9_-]{3,50}") || voucher.getAmount()==null || voucher.getAmount().signum()<=0 || !Set.of("FIXED","PERCENT").contains(voucher.getType()) || "PERCENT".equals(voucher.getType()) && voucher.getAmount().compareTo(new BigDecimal("100"))>0 || voucher.getMinimumAmount()==null || voucher.getMinimumAmount().signum()<0 || voucher.getMaximumDiscount()!=null && voucher.getMaximumDiscount().signum()<0 || voucher.getUsageLimit()!=null && voucher.getUsageLimit()<1 || voucher.getPerCustomerLimit()!=null && voucher.getPerCustomerLimit()<1 || voucher.getStartsAt()!=null && voucher.getExpiresAt()!=null && !voucher.getStartsAt().isBefore(voucher.getExpiresAt())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Cấu hình voucher không hợp lệ.");
        voucher.setCode(voucher.getCode().trim().toUpperCase(Locale.ROOT));
        if(vouchers.findByCode(voucher.getCode()).isPresent())throw new ResponseStatusException(HttpStatus.CONFLICT,"Mã đã tồn tại.");
        voucher.setId(null);voucher.setUsedCount(0);return vouchers.save(voucher);
    }
}
