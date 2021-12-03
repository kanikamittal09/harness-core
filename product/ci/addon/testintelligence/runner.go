// TestRunner provides the test command and java agent config
// which is needed to run test intelligence.
package testintelligence

import (
	"context"
	"github.com/wings-software/portal/product/ci/ti-service/types"
)

//go:generate mockgen -source runner.go -package=testintelligence -destination mocks/runner_mock.go TestRunner
type TestRunner interface {
	// Get the command which needs to be executed to run only the specified tests.
	// tests: list of selected tests which need to be executed
	// agentConfigPath: path to the java agent config. This needs to be added to the
	// command if instrumentation is required.
	// ignoreInstr: instrumentation might not be required in some cases like manual executions
	// runAll: if there was any issue in figuring out which tests to run, this parameter is set as true
	GetCmd(ctx context.Context, tests []types.RunnableTest, userArgs, agentConfigPath string, ignoreInstr, runAll bool) (string, error)

	// Auto detect the list of packages to be instrumented.
	// Return an error if we could not detect or if it's unimplemented.
	AutoDetectPackages() ([]string, error)
}
