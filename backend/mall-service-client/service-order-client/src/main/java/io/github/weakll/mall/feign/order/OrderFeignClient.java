package io.github.weakll.mall.feign.order;

import io.github.weakll.mall.model.entity.order.OrderInfo;
import io.github.weakll.mall.model.vo.common.PageResult;
import io.github.weakll.mall.model.vo.common.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;


@FeignClient(value = "service-order")
public interface OrderFeignClient {

    @GetMapping("/api/order/orderInfo/auth/getOrderInfoByOrderNo/{orderNo}")
    public Result<OrderInfo> getOrderInfoByOrderNo(@PathVariable String orderNo) ;

    /**
     * 查询当前登录用户自己的订单列表。
     *
     * <p>服务端 {@code OrderInfoService.findUserPage} 按当前登录用户过滤，
     * 因此本方法天然只返回调用者自己的订单——AI 客服的"用户数据隔离"
     * 由此在服务端成立，而不是靠提示词约束模型。
     *
     * <p>返回 {@code Result<PageResult<OrderInfo>>}：服务端返回的 JSON 中
     * {@code data} 是 {@code {"total":n,"list":[...]}} 形式的分页对象。
     * 若写成 {@code Result<List<OrderInfo>>}，Jackson 会反序列化失败，
     * 表现为本方法抛异常、上游报"下游 500"。原因详见 {@link PageResult}。
     *
     * @param page        页码，从 1 开始
     * @param limit       每页条数
     * @param orderStatus 订单状态筛选，可为空表示全部
     */
    @GetMapping("/api/order/orderInfo/auth/{page}/{limit}")
    Result<PageResult<OrderInfo>> listMyOrders(@PathVariable("page") Integer page,
                                               @PathVariable("limit") Integer limit,
                                               @RequestParam(value = "orderStatus", required = false) Integer orderStatus);

    @GetMapping("/api/order/orderInfo/auth/updateOrderStatusPayed/{orderNo}/{orderStatus}")
    public Result<Object> updateOrderStatus(@PathVariable(value = "orderNo") String orderNo , @PathVariable(value = "orderStatus") Integer orderStatus);

}
