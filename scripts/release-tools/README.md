# Release Tools

This directory contains scripts for analyzing and reporting on Rundeck releases and deployed pull requests.

## Scripts

### `generate_pr_report.py`

Generates a comprehensive report of pull requests deployed to production between specified dates.

**Features:**
- Lists all releases within a date range
- Extracts PR numbers from git commits between releases
- Generates a Markdown report with links to all PRs
- Provides summary statistics

**Usage:**

The script is configured with hardcoded dates. To modify the date range, edit the variables at the bottom of the script:

```python
start_date = "2024-11-01"  # Format: YYYY-MM-DD
end_date = "2025-10-03"    # Format: YYYY-MM-DD
```

Then run:

```bash
cd /home/runner/work/rundeck/rundeck
python3 scripts/release-tools/generate_pr_report.py
```

**Output:**

The script generates a file named `deployed_prs_report.md` in the repository root containing:
- List of all releases in the date range
- Pull requests included in each release (extracted from commit messages)
- Summary statistics (total releases, total unique PRs)

### `list_deployed_prs.py`

A more flexible command-line script for generating PR reports (requires direct GitHub API access).

**Usage:**

```bash
python3 scripts/release-tools/list_deployed_prs.py \
  --start-date 2024-11-01 \
  --end-date 2025-10-03 \
  --output my_report.md
```

**Note:** This script requires direct access to the GitHub API which may not be available in all environments due to network restrictions.

## Requirements

- Python 3.6+
- Git repository with tags
- For `list_deployed_prs.py`: `requests` library (`pip install requests`)

## Example Report

The generated report includes:

```markdown
# Deployed Pull Requests to Production

**Repository:** rundeck/rundeck
**Date Range:** 2024-11-01 to 2025-10-03
**Total Releases:** 11

---

## v5.8.0-20241205

- **Tag:** `v5.8.0`
- **Published:** 2024-12-05T22:18:25Z
- **URL:** https://github.com/rundeck/rundeck/releases/tag/v5.8.0

### Pull Requests (25)

- [#9282](https://github.com/rundeck/rundeck/pull/9282)
- [#9335](https://github.com/rundeck/rundeck/pull/9335)
...

## Summary

- **Total Releases:** 11
- **Total Unique Pull Requests:** 190
```

## How It Works

1. **Release Data**: The script reads release information (tags, dates, names) from hardcoded JSON data
2. **Date Filtering**: Filters releases to only include those published within the specified date range
3. **PR Extraction**: For each release, runs `git log` to get commits between the previous and current release tag
4. **Pattern Matching**: Extracts PR numbers from commit messages using regex patterns (e.g., `#1234`, `(#1234)`)
5. **Report Generation**: Creates a formatted Markdown report with all findings

## Notes

- PR numbers are extracted from commit messages, so they rely on consistent commit message formatting
- The script automatically finds the previous release tag for comparison
- Duplicate PR numbers across releases are tracked and deduplicated in the summary
- Release candidates (RC) tags are used when available for more accurate PR attribution
