package io.github.weakll.mall.user.service.impl;

import io.github.weakll.mall.model.entity.user.UserAddress;
import io.github.weakll.mall.user.mapper.UserAddressMapper;
import io.github.weakll.mall.user.service.UserAddressService;
import io.github.weakll.mall.utils.AuthContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class UserAddressServiceImpl implements UserAddressService {

    @Autowired
    private UserAddressMapper userAddressMapper;

    @Override
    public List<UserAddress> findUserAddressList() {
        Long userId = AuthContextUtil.getUserInfo().getId();
        return userAddressMapper.findByUserId(userId);
    }

    //业务接口实现
    @Override
    public UserAddress getById(Long id) {
        return userAddressMapper.getById(id);
    }

    @Override
    public void save(UserAddress userAddress) {
        Long userId = AuthContextUtil.getUserInfo().getId();
        userAddress.setUserId(userId);

        if (userAddress.getIsDefault() == null) {
            userAddress.setIsDefault(0);
        }

        if (userAddress.getIsDefault() == 1) {
            cancelDefaultAddresses(userId);
        }

        userAddressMapper.save(userAddress);
    }

    @Override
    public void updateById(UserAddress userAddress) {
        if (userAddress.getIsDefault() == 1) {
            Long userId = AuthContextUtil.getUserInfo().getId();
            cancelDefaultAddresses(userId);
        }
        userAddressMapper.updateById(userAddress);
    }

    @Override
    public void removeById(Long id) {
        Long userId = AuthContextUtil.getUserInfo().getId();
        userAddressMapper.deleteById(id, userId);
    }

    @Override
    public void deleteById(Long id) {

    }

    private void cancelDefaultAddresses(Long userId) {
        List<UserAddress> addresses = userAddressMapper.findByUserId(userId);
        for (UserAddress address : addresses) {
            if (address.getIsDefault() == 1) {
                address.setIsDefault(0);
                userAddressMapper.updateById(address);
            }
        }
    }
}
