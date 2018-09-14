package com.apple.wwrc.service.customer;

import com.apple.wwrc.foundation.framework.annotation.ApiParam;
import com.apple.wwrc.foundation.framework.annotation.POSApi;
import com.apple.wwrc.foundation.framework.annotation.POSMethod;
import com.apple.wwrc.foundation.framework.annotation.POSService;
import com.apple.wwrc.foundation.framework.exception.FrameworkException;
import com.apple.wwrc.service.customer.model.response.EmployeeSearch;

/**
 *  This is your service common class. Make sure it's documented here. Added more comments here. Add more
 */
@POSService(name="CustomerService", version="v1.0")
public interface CustomerServiceInterface {

	// exposed public APIs (Will e availabe at gateway)
	@POSApi(name="searchEmployee", desc="Employee lookup api.")
	@POSMethod()
	public EmployeeSearch employeeLookup(@ApiParam(value="query", desc="encrypted badgeId or email address")String query,
								 @ApiParam(value="page", desc="page number (begin with 0)")int page,
								 @ApiParam(value="limit", desc="page size") int limit) throws FrameworkException;
}
