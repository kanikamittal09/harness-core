// Copyright 2020 Harness Inc. All rights reserved.
// Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
// that can be found in the licenses directory at the root of this repository, also available at
// https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.

// Code generated by MockGen. DO NOT EDIT.
// Source: scm_client.go

// Package grpcclient is a generated GoMock package.
package grpcclient

import (
	gomock "github.com/golang/mock/gomock"
	proto "github.com/harness/harness-core/product/ci/scm/proto"
	reflect "reflect"
)

// MockSCMClient is a mock of SCMClient interface.
type MockSCMClient struct {
	ctrl     *gomock.Controller
	recorder *MockSCMClientMockRecorder
}

// MockSCMClientMockRecorder is the mock recorder for MockSCMClient.
type MockSCMClientMockRecorder struct {
	mock *MockSCMClient
}

// NewMockSCMClient creates a new mock instance.
func NewMockSCMClient(ctrl *gomock.Controller) *MockSCMClient {
	mock := &MockSCMClient{ctrl: ctrl}
	mock.recorder = &MockSCMClientMockRecorder{mock}
	return mock
}

// EXPECT returns an object that allows the caller to indicate expected use.
func (m *MockSCMClient) EXPECT() *MockSCMClientMockRecorder {
	return m.recorder
}

// CloseConn mocks base method.
func (m *MockSCMClient) CloseConn() error {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "CloseConn")
	ret0, _ := ret[0].(error)
	return ret0
}

// CloseConn indicates an expected call of CloseConn.
func (mr *MockSCMClientMockRecorder) CloseConn() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "CloseConn", reflect.TypeOf((*MockSCMClient)(nil).CloseConn))
}

// Client mocks base method.
func (m *MockSCMClient) Client() proto.SCMClient {
	m.ctrl.T.Helper()
	ret := m.ctrl.Call(m, "Client")
	ret0, _ := ret[0].(proto.SCMClient)
	return ret0
}

// Client indicates an expected call of Client.
func (mr *MockSCMClientMockRecorder) Client() *gomock.Call {
	mr.mock.ctrl.T.Helper()
	return mr.mock.ctrl.RecordCallWithMethodType(mr.mock, "Client", reflect.TypeOf((*MockSCMClient)(nil).Client))
}
