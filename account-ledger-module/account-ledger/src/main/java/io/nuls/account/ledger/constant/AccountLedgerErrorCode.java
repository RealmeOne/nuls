/*
 * MIT License
 *
 * Copyright (c) 2017-2018 nuls.io
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package io.nuls.account.ledger.constant;

import io.nuls.kernel.constant.ErrorCode;

/**
 * @author: Niels Wang
 * @date: 2018/5/5
 */
public interface AccountLedgerErrorCode {

    ErrorCode ACCOUNT_NOT_EXIST = ErrorCode.init("AL001", "90001");
    ErrorCode ADDRESS_ERROR = ErrorCode.init("AL004", "90004");
    ErrorCode SUCCESS = ErrorCode.init("AL010", "90010");
    ErrorCode FAILED = ErrorCode.init("AL011", "90011");
    ErrorCode PARAMETER_ERROR = ErrorCode.init("AL012", "90012");
    ErrorCode IO_ERROR = ErrorCode.init("AL013", "90013");
    ErrorCode SOURCE_TX_NOT_EXSITS = ErrorCode.init("AL014", "90014");
    ErrorCode UNKNOW_ERROR = ErrorCode.init("AL015", "90015");
}
