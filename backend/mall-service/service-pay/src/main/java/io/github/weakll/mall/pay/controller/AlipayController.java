package io.github.weakll.mall.pay.controller;

import io.github.weakll.mall.model.vo.common.Result;
import io.github.weakll.mall.model.vo.common.ResultCodeEnum;
import io.github.weakll.mall.pay.properties.AlipayProperties;
import io.github.weakll.mall.pay.service.AlipayService;
import io.github.weakll.mall.pay.service.PaymentInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@Controller
@RequestMapping("/api/order/alipay")
public class AlipayController {
    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Autowired
    private AlipayProperties alipayProperties;

    @Operation(summary="虚拟支付下单（返回虚拟收银台确认页面）")
    @GetMapping("submitAlipay/{orderNo}")
    @ResponseBody
    public Result<String> submitAlipay(@Parameter(name = "orderNo", description = "订单号", required = true) @PathVariable(value = "orderNo") String orderNo,
                                       HttpServletRequest request) {
        // 构造完整的网关地址前缀（用于表单提交）
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String baseUrl = scheme + "://" + serverName;
        if (serverPort != 80 && serverPort != 443) {
            baseUrl += ":" + serverPort;
        }
        // 使用配置的前端地址（用于支付成功后跳转）
        String frontendUrl = alipayProperties.getFrontendUrl();
        if (frontendUrl == null || frontendUrl.isEmpty()) {
            frontendUrl = "http://127.0.0.1:5173";
        }
        String form = alipayService.submitAlipay(orderNo, baseUrl, frontendUrl);
        return Result.build(form, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary="虚拟支付一键完成（跳过收银台）")
    @GetMapping("directPay/{orderNo}")
    @ResponseBody
    public Result<String> directPay(@PathVariable(value = "orderNo") String orderNo) {
        log.info("虚拟支付一键完成，订单号：{}", orderNo);

        // 1. 保存支付记录
        paymentInfoService.savePaymentInfo(orderNo);

        // 2. 直接构造支付成功参数并更新状态
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", orderNo);
        paramMap.put("trade_no", "VIRTUAL_" + System.currentTimeMillis());
        paramMap.put("trade_status", "TRADE_SUCCESS");
        paramMap.put("total_amount", paymentInfoService.savePaymentInfo(orderNo).getAmount().toPlainString());
        paymentInfoService.updatePaymentStatus(paramMap, 2);

        return Result.build(null, ResultCodeEnum.SUCCESS);
    }

    @Operation(summary="虚拟支付确认")
    @PostMapping("confirmVirtualPay/{orderNo}")
    public void confirmVirtualPay(@PathVariable(value = "orderNo") String orderNo,
                                  HttpServletResponse response) throws IOException {
        log.info("虚拟支付确认，订单号：{}", orderNo);

        // 构造支付成功参数
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("out_trade_no", orderNo);
        paramMap.put("trade_no", "VIRTUAL_" + System.currentTimeMillis());
        paramMap.put("trade_status", "TRADE_SUCCESS");
        paramMap.put("total_amount", paymentInfoService.savePaymentInfo(orderNo).getAmount().toPlainString());

        // 更新支付状态、订单状态、商品销量
        paymentInfoService.updatePaymentStatus(paramMap, 2);

        // 使用配置的前端地址构造订单列表页链接
        String frontendUrl = alipayProperties.getFrontendUrl();
        if (frontendUrl == null || frontendUrl.isEmpty()) {
            frontendUrl = "http://127.0.0.1:5173";
        }
        String detailUrl = frontendUrl + "/#/pages/order/order?state=0";

        // 直接返回支付成功HTML页面
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html><head>");
        out.println("<meta charset='UTF-8'>");
        out.println("<meta name='viewport' content='width=device-width, initial-scale=1.0'>");
        out.println("<title>支付成功</title>");
        out.println("<style>");
        out.println("  * { margin: 0; padding: 0; box-sizing: border-box; }");
        out.println("  body { font-family: 'Microsoft YaHei', Arial, sans-serif; }");
        out.println("  .overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background: #f5f5f5; z-index: 999999; padding: 20px; }");
        out.println("  .card { text-align: center; background: white; padding: 40px 30px; border-radius: 12px; box-shadow: 0 2px 12px rgba(0,0,0,0.1); max-width: 360px; width: 100%; }");
        out.println("  .icon { width: 60px; height: 60px; background: #4caf50; border-radius: 50%; display: inline-flex; align-items: center; justify-content: center; margin-bottom: 15px; }");
        out.println("  .icon::after { content: '✓'; color: white; font-size: 30px; font-weight: bold; }");
        out.println("  h1 { color: #333; font-size: 20px; margin-bottom: 8px; }");
        out.println("  p { color: #888; font-size: 13px; margin: 4px 0; }");
        out.println("  .order-no { color: #bbb; font-size: 11px; margin-top: 10px; word-break: break-all; }");
        out.println("  .btn { display: block; width: 100%; margin-top: 20px; padding: 11px; background: #2196f3; color: white; text-decoration: none; border-radius: 6px; font-size: 15px; }");
        out.println("  .btn:active { background: #1976d2; }");
        out.println("</style></head><body>");
        out.println("<div class='overlay'>");
        out.println("  <div class='card'>");
        out.println("    <div class='icon'></div>");
        out.println("    <h1>支付成功！</h1>");
        out.println("    <p>您的订单已完成支付</p>");
        out.println("    <p class='order-no'>订单号：" + orderNo + "</p>");
        out.println("    <a class='btn' href='" + detailUrl + "'>查看我的订单</a>");
        out.println("  </div>");
        out.println("</div></body></html>");
        out.flush();
        out.close();
    }
}
