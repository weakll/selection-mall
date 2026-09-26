package io.github.weakll.mall.pay.service.impl;

import io.github.weakll.mall.model.entity.pay.PaymentInfo;
import io.github.weakll.mall.pay.service.AlipayService;
import io.github.weakll.mall.pay.service.PaymentInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AlipayServiceImpl implements AlipayService {

    @Autowired
    private PaymentInfoService paymentInfoService;

    @Override
    public String submitAlipay(String orderNo, String baseUrl, String frontendUrl) {
        // 保存支付记录
        PaymentInfo paymentInfo = paymentInfoService.savePaymentInfo(orderNo);

        // 拼接完整的确认支付地址（POST 提交到后端）
        String confirmUrl = baseUrl + "/api/order/alipay/confirmVirtualPay/" + orderNo;

        // 返回虚拟支付收银台HTML（全屏覆盖层）
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>虚拟支付收银台</title>\n");
        html.append("    <style>\n");
        html.append("        * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("        body { font-family: 'Microsoft YaHei', Arial, sans-serif; background: #f5f5f5; }\n");
        html.append("        .overlay { position: fixed; top: 0; left: 0; width: 100vw; height: 100vh; display: flex; justify-content: center; align-items: center; background: #f5f5f5; z-index: 999999; padding: 20px; }\n");
        html.append("        .pay-wrap { max-width: 400px; width: 100%; margin: 0 auto; background: white; border-radius: 10px; padding: 30px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("        h2 { color: #333; text-align: center; font-size: 18px; margin-bottom: 20px; }\n");
        html.append("        .info { background: #f9f9f9; padding: 15px; border-radius: 8px; margin-bottom: 20px; }\n");
        html.append("        .info p { margin: 8px 0; color: #666; font-size: 14px; }\n");
        html.append("        .info .label { color: #999; }\n");
        html.append("        .amount { font-size: 32px; color: #ff5722; text-align: center; margin: 20px 0; font-weight: bold; }\n");
        html.append("        .btn-pay { display: block; width: 100%; padding: 15px; background: #4caf50; color: white; border: none; border-radius: 8px; font-size: 16px; cursor: pointer; }\n");
        html.append("        .btn-pay:active { background: #45a049; }\n");
        html.append("        .tips { text-align: center; color: #bbb; font-size: 12px; margin-top: 15px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("    <div class='overlay'>\n");
        html.append("        <div class='pay-wrap'>\n");
        html.append("            <h2>虚拟支付收银台</h2>\n");
        html.append("            <div class='info'>\n");
        html.append("                <p><span class='label'>订单编号：</span>").append(paymentInfo.getOrderNo()).append("</p>\n");
        html.append("                <p><span class='label'>商品信息：</span>").append(paymentInfo.getContent()).append("</p>\n");
        html.append("            </div>\n");
        html.append("            <div class='amount'>&yen;").append(paymentInfo.getAmount()).append("</div>\n");
        html.append("            <form action='").append(confirmUrl).append("' method='post'>\n");
        html.append("                <button type='submit' class='btn-pay'>确认支付</button>\n");
        html.append("            </form>\n");
        html.append("            <p class='tips'>本页面为虚拟支付，仅用于演示，不会扣除真实金额</p>\n");
        html.append("        </div>\n");
        html.append("    </div>\n");
        html.append("</body>\n");
        html.append("</html>");

        log.info("虚拟支付收银台页面生成成功，订单号：{}", orderNo);
        return html.toString();
    }
}
