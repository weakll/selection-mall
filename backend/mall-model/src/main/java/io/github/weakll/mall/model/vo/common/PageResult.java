package io.github.weakll.mall.model.vo.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

/**
 * 下游服务分页响应的最小视图。
 *
 * <p><b>为什么需要它：</b>商城各服务的列表接口统一返回 {@code Result<PageInfo<T>>}，
 * 其中 {@code PageInfo} 由 pagehelper 提供，序列化后形如
 * {@code data:{"total":43,"list":[...]}}。若调用方把返回类型写成
 * {@code Result<List<T>>}，Jackson 会因 {@code data} 是对象而非数组而失败，
 * 表现为下游返回 500——容易被误判为"下游服务坏了"。
 *
 * <p>放在 mall-model 而非某个服务内部：Feign 客户端模块需要引用它，
 * 而客户端模块只依赖 mall-model。这里只声明必要字段并忽略其余分页元数据，
 * 因此既不引入 pagehelper 依赖，也不受其字段变化影响。
 *
 * @param total 总记录数
 * @param list  当前页记录
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long total;

    private List<T> list;

    public PageResult() {
    }

    public PageResult(Long total, List<T> list) {
        this.total = total;
        this.list = list;
    }

    /** 永不为 null 的列表视图，省去调用方判空。 */
    public List<T> listOrEmpty() {
        return list == null ? List.of() : list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
