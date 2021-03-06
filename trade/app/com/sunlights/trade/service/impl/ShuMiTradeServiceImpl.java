package com.sunlights.trade.service.impl;

import com.google.common.collect.Lists;
import com.sunlights.account.service.AccountService;
import com.sunlights.account.service.CapitalService;
import com.sunlights.account.service.impl.AccountServiceImpl;
import com.sunlights.account.service.impl.CapitalServiceImpl;
import com.sunlights.common.DictConst;
import com.sunlights.common.MsgCode;
import com.sunlights.common.Severity;
import com.sunlights.common.service.ParameterService;
import com.sunlights.common.utils.CommonUtil;
import com.sunlights.common.utils.DBHelper;
import com.sunlights.common.utils.MessageUtil;
import com.sunlights.common.vo.Message;
import com.sunlights.common.vo.MessageHeaderVo;
import com.sunlights.core.service.OpenAccountPactService;
import com.sunlights.core.service.ProductService;
import com.sunlights.core.service.impl.OpenAccountPactServiceImpl;
import com.sunlights.core.service.impl.ProductServiceImpl;
import com.sunlights.customer.service.BankCardService;
import com.sunlights.customer.service.BankService;
import com.sunlights.customer.service.impl.BankCardServiceImpl;
import com.sunlights.customer.service.impl.BankServiceImpl;
import com.sunlights.customer.service.impl.CustomerService;
import com.sunlights.customer.vo.BankCardVo;
import com.sunlights.trade.dal.TradeDao;
import com.sunlights.trade.dal.impl.TradeDaoImpl;
import com.sunlights.trade.service.ShuMiTradeService;
import com.sunlights.trade.service.TradeStatusChangeService;
import com.sunlights.trade.vo.ShuMiTradeFormVo;
import models.Customer;
import models.FundNav;
import models.Trade;
import org.joda.time.LocalDate;
import play.Logger;
import services.DateCalcService;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * <p>Project: financeplatform</p>
 * <p>Title: ShuMiTradeServiceImpl.java</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2014 Sunlights.cc</p>
 * <p>All Rights Reserved.</p>
 *
 * @author <a href="mailto:jiaming.wang@sunlights.cc">wangJiaMing</a>
 */
public class ShuMiTradeServiceImpl implements ShuMiTradeService {
    private TradeDao tradeDao = new TradeDaoImpl();
    private CustomerService customerService = new CustomerService();
    private CapitalService capitalService = new CapitalServiceImpl();
    private OpenAccountPactService openAccountPactService = new OpenAccountPactServiceImpl();
    private AccountService accountService = new AccountServiceImpl();
    private ProductService productService = new ProductServiceImpl();
    private BankCardService bankCardService = new BankCardServiceImpl();
    private BankService bankService = new BankServiceImpl();
    private ParameterService parameterService = new ParameterService();
    private DateCalcService dateCalcService = new DateCalcService();
    private TradeStatusChangeService tradeStatusChangeService = new TradeStatusChangeServiceImpl();

    @Override
    public List<MessageHeaderVo> shuMiTradeOrder(ShuMiTradeFormVo shuMiTradeFormVo, String token) {
        Customer customer = customerService.getCustomerByToken(token);
        String customerId = customer.getCustomerId();

        //开户银行卡信息
        if (shuMiTradeFormVo.getBankAcco() != null) {
            createOpenAccountBankInfo(shuMiTradeFormVo, customerId);
        }

        FundNav fundNav = productService.findFundNavByCode(shuMiTradeFormVo.getFundCode());
        String companyId = null;
        if (fundNav == null) {
            MessageUtil.getInstance().setMessage(new Message(Severity.WARN, MsgCode.TRADE_ORDER_NOCODE));
        } else {
            companyId = fundNav.getIaGuid();
        }

        //子帐号
        accountService.createSubAccount(customerId, companyId, DictConst.FP_PRODUCT_TYPE_1);

        //下单记录
        Trade trade = createTrade(shuMiTradeFormVo, customerId, DictConst.TRADE_TYPE_1, fundNav);

        //产品申购人数+1
        productService.addProductPurchasedNum(shuMiTradeFormVo.getFundCode());
        //产品刷新缓存
        productService.refreshProductListCache();
        //生成收益日期线
        tradeStatusChangeService.createTradeStatusChange(trade);

        List<MessageHeaderVo> list = buildMessageHeaderVos(shuMiTradeFormVo, customer, customerId, trade);
        return list;

    }

    private List<MessageHeaderVo> buildMessageHeaderVos(ShuMiTradeFormVo shuMiTradeFormVo, Customer customer, String customerId, Trade trade) {
        List<MessageHeaderVo> list = Lists.newArrayList();
        MessageHeaderVo messageHeaderVo = new MessageHeaderVo(DictConst.PUSH_TYPE_3, null, customerId);

        Date tradeTime = trade.getTradeTime();

        LocalDate confirmLocalDate = dateCalcService.getEndTradeDate(CommonUtil.dateToString(tradeTime, CommonUtil.DATE_FORMAT_LONG), 1);
        LocalDate earningLocalDate = dateCalcService.getEndTradeDate(CommonUtil.dateToString(tradeTime, CommonUtil.DATE_FORMAT_LONG), 2);
        
        messageHeaderVo.buildParams(customer.getRealName(), shuMiTradeFormVo.getFundName(),
                confirmLocalDate.toString(CommonUtil.DATE_FORMAT_SHORT),
                earningLocalDate.toString(CommonUtil.DATE_FORMAT_SHORT));
        list.add(messageHeaderVo);
        return list;
    }

    @Override
    public String shuMiTradeRedeem(ShuMiTradeFormVo shuMiTradeFormVo, String token) {
        Customer customer = customerService.getCustomerByToken(token);
        String customerId = customer.getCustomerId();
        FundNav fundNav = productService.findFundNavByCode(shuMiTradeFormVo.getFundCode());
        if (fundNav == null) {
            MessageUtil.getInstance().setMessage(new Message(Severity.WARN,
                    MsgCode.TRADE_REDEEM_NOCODE.getCode(), MsgCode.TRADE_REDEEM_NOCODE.getMessage(), MsgCode.TRADE_REDEEM_NOCODE.getDetail()));
        }
        Trade trade = createTrade(shuMiTradeFormVo, customerId, DictConst.TRADE_TYPE_2, fundNav);

        //生成收益日期线
        tradeStatusChangeService.createTradeStatusChange(trade);

        List<MessageHeaderVo> list = Lists.newArrayList();
        MessageHeaderVo messageHeaderVo = new MessageHeaderVo(DictConst.PUSH_TYPE_3, null, customerId);
        messageHeaderVo.buildParams(customer.getRealName(), shuMiTradeFormVo.getFundName(), trade.getTradeAmount().toString(), trade.getBankName());
        list.add(messageHeaderVo);
        return MessageUtil.getInstance().setMessageHeader(list);
    }

    private void createOpenAccountBankInfo(ShuMiTradeFormVo shuMiTradeFormVo, String customerId) {
        String bankName = shuMiTradeFormVo.getBankName();
        String bankCardNo = shuMiTradeFormVo.getBankAcco();

        Logger.debug("bankName:" + bankName);
        Logger.debug("bankCardNo:" + bankCardNo);

        BankCardVo bankCardVo = new BankCardVo();
        bankCardVo.setBankCard(bankCardNo);
        bankCardVo.setBankName(bankName);
//        Bank bank = bankService.findBankByBankName(bankName);
//        bankCardVo.setBankCode(bank.getBankCode());
        openAccountPactService.createFundOpenAccount(customerId, bankCardVo);
    }

    private Trade createTrade(ShuMiTradeFormVo shuMiTradeFormVo, String customerId, String type, FundNav fundNav) {
        String applySum = shuMiTradeFormVo.getApplySum();
        String fundCode = shuMiTradeFormVo.getFundCode();
        String fundName = shuMiTradeFormVo.getFundName();
        String bankName = shuMiTradeFormVo.getBankName() == null ? shuMiTradeFormVo.getBankCardInfo() : shuMiTradeFormVo.getBankName();
        String bankCardNo = shuMiTradeFormVo.getBankAcco();
        String applySerial = shuMiTradeFormVo.getApplySerial();

        Timestamp currentTime = DBHelper.getCurrentTime();
        Date dateTime = null;
        try {
            dateTime = CommonUtil.stringToDate(shuMiTradeFormVo.getDateTime(), CommonUtil.DATE_FORMAT_SHUMI);
        } catch (ParseException e) {
            e.printStackTrace();
            dateTime = currentTime;
        }

        Trade trade = new Trade();
        trade.setTradeNo(applySerial);
        trade.setType(type);
        trade.setTradeAmount(new BigDecimal(applySum));
        trade.setTradeStatus(DictConst.TRADE_STATUS_1);//途中
        trade.setConfirmStatus(DictConst.VERIFY_STATUS_1);//1-待确认
        trade.setTradeTime(dateTime);
        trade.setCreateTime(currentTime);
        trade.setCustId(customerId);
        trade.setBankCardNo(bankCardNo);
        trade.setBankName(bankName);
        //数米 申购付款成功才回调
        if (DictConst.TRADE_TYPE_1.equals(type)) {
            trade.setPayStatus(DictConst.PAYMENT_STATUS_3);//未付款
        } else {//赎回
            trade.setPayStatus(DictConst.PAYMENT_STATUS_2);
        }
        trade.setProductCode(fundCode);
        trade.setProductName(fundName);

        if (fundNav != null) {
            if (fundNav.getPurchaseLimitMin() != null) {
                trade.setProductPrice(fundNav.getPurchaseLimitMin());
//                trade.setQuantity(Integer.valueOf(new BigDecimal(applySum).divide(fundNav.getPurchaseLimitMin()).toString()));
            }
            BigDecimal fee = BigDecimal.ZERO;
            BigDecimal chargeRateValue = fundNav.getChargeRateValue();
            BigDecimal fundNavDiscount = fundNav.getDiscount();
            if (chargeRateValue != null && BigDecimal.ZERO.compareTo(chargeRateValue) != 0 && fundNavDiscount != null) {
                fee = fundNavDiscount;
            }
            trade.setFee(fee);
        }

        return tradeDao.saveTrade(trade);
    }



}
