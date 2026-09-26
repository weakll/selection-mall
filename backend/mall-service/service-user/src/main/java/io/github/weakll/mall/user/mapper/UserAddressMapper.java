package io.github.weakll.mall.user.mapper;

import io.github.weakll.mall.model.entity.user.UserAddress;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
@Mapper
public interface UserAddressMapper {
    List<UserAddress> findByUserId(Long userId);

    UserAddress getById(Long id);

    void save(UserAddress userAddress);

    void updateById(UserAddress userAddress);

    void deleteById(Long id, Long userId);
}
