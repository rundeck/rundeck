# Deployment Report Summary

## Overview

This document summarizes the deployed pull requests to production for the `rundeck/rundeck` repository between **November 1, 2024** and **October 3, 2025**.

## Key Statistics

- **Date Range:** November 1, 2024 - October 3, 2025
- **Total Production Releases:** 11
- **Total Unique Pull Requests Deployed:** 190
- **Report File:** `deployed_prs_report.md`

## Releases Included

The following production releases were deployed during this period:

1. **v5.8.0** (2024-12-05) - 25 PRs
2. **v5.9.0** (2025-02-05) - 39 PRs
3. **v5.10.0** (2025-03-12) - 29 PRs
4. **v5.11.0** (2025-04-09) - 20 PRs
5. **v5.10.1** (2025-04-15) - 2 PRs
6. **v5.11.1** (2025-04-15) - 2 PRs
7. **v5.12.0** (2025-05-12) - 21 PRs
8. **v5.13.0** (2025-06-25) - 23 PRs
9. **v5.14.0** (2025-08-04) - 17 PRs
10. **v5.14.1** (2025-08-18) - 1 PR
11. **v5.15.0** (2025-09-02) - 27 PRs

## How to Use This Report

### Viewing the Full Report

The complete report with all PR numbers and links is available in:
```
deployed_prs_report.md
```

This report contains:
- Each release with its publication date and GitHub URL
- Complete list of PR numbers for each release
- Direct links to each PR on GitHub
- Release notes for each version

### Regenerating the Report

To regenerate the report with different date ranges:

1. Edit `scripts/release-tools/generate_pr_report.py`
2. Modify the date range variables:
   ```python
   start_date = "YYYY-MM-DD"
   end_date = "YYYY-MM-DD"
   ```
3. Run the script:
   ```bash
   python3 scripts/release-tools/generate_pr_report.py
   ```

See `scripts/release-tools/README.md` for detailed usage instructions.

## Notes

- PR numbers are extracted from git commit messages between release tags
- Some PRs may appear in multiple releases if they were cherry-picked or backported
- The report includes production releases only (excludes release candidates and draft releases)
- Total unique PRs counts each PR number only once, even if it appears in multiple releases

## Tools and Scripts

This report was generated using custom Python scripts located in:
- `scripts/release-tools/generate_pr_report.py` - Main report generator
- `scripts/release-tools/list_deployed_prs.py` - Alternative CLI-based generator

Both scripts are documented in `scripts/release-tools/README.md`.
