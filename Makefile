.DEFAULT_GOAL := help

.PHONY: help build test test-unit smoke clean-build artifact deploy download server lint version

help:
	@echo "Everything plugin development helpers"
	@echo ""
	@echo "  build          Build the shaded plugin jar with Gradle"
	@echo "  test           Run the JUnit test suite"
	@echo "  test-unit      Run the project's available unit/automated tests"
	@echo "  smoke          Build and run the automated test suite"
	@echo "  clean-build    Run Gradle clean build"
	@echo "  artifact       Print the latest shaded plugin artifact"
	@echo "  deploy         Copy only the newest shaded jar into run/plugins/"
	@echo "  download       Download/refresh Paper and plugin jars for the dev server"
	@echo "  server         Use the current deployed plugin and start the Paper server"
	@echo "  validate       Build and test the project"
	@echo "  version        Print the configured project version"

build:
	@./gradlew build

test: test-unit

test-unit:
	@./gradlew test

smoke: validate

clean-build:
	@./gradlew clean build

version:
	@sed -n 's/^version *= *"\([^"]*\)".*/\1/p' build.gradle.kts

artifact:
	@set -e; \
	latest="$$(ls -t build/libs/everything-*-b*.jar 2>/dev/null | head -n 1)"; \
	if [ -z "$$latest" ]; then \
		echo "No shaded jar found. Run 'make build' first." >&2; \
		exit 1; \
	fi; \
	printf '%s\n' "$$latest"

deploy: build
	@set -e; \
	mkdir -p run/plugins; \
	latest="$$(ls -t build/libs/everything-*-b*.jar 2>/dev/null | head -n 1)"; \
	if [ -z "$$latest" ]; then \
		echo "No shaded jar found after build." >&2; \
		exit 1; \
	fi; \
	rm -f run/plugins/everything-*.jar; \
	cp "$$latest" run/plugins/; \
	echo "Deployed $$(basename "$$latest")"

download:
	@./run/download.sh

server: download
	@cd run && ./run.sh

validate: build test-unit