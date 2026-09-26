package io.github.weakll.mall.pay.service;

public interface AlipayService {
    String submitAlipay(String orderNo, String baseUrl, String frontendUrl);
}
