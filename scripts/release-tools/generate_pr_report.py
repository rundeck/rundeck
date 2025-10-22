#!/usr/bin/env python3
"""
Generate a report of deployed PRs based on release data.
This script processes release information gathered via GitHub MCP tools.
"""

import json
import sys
from datetime import datetime
from typing import List, Dict, Any


def parse_release_date(date_str: str) -> datetime:
    """Parse GitHub API date format."""
    return datetime.strptime(date_str, "%Y-%m-%dT%H:%M:%SZ")


def filter_releases_by_date(releases: List[Dict[str, Any]], start_date: str, end_date: str) -> List[Dict[str, Any]]:
    """Filter releases by date range."""
    start = datetime.strptime(start_date, "%Y-%m-%d")
    end = datetime.strptime(end_date, "%Y-%m-%d")
    
    filtered = []
    for release in releases:
        if release.get("draft", False):
            continue
        
        published_at_str = release.get("published_at")
        if not published_at_str:
            continue
        
        published_at = parse_release_date(published_at_str)
        
        if start <= published_at <= end:
            filtered.append(release)
    
    return sorted(filtered, key=lambda r: r["published_at"])


def generate_report(releases: List[Dict[str, Any]], start_date: str, end_date: str, output_file: str):
    """Generate markdown report of releases."""
    
    lines = []
    lines.append(f"# Deployed Pull Requests to Production\n\n")
    lines.append(f"**Repository:** rundeck/rundeck\n\n")
    lines.append(f"**Date Range:** {start_date} to {end_date}\n\n")
    lines.append(f"**Total Releases:** {len(releases)}\n\n")
    lines.append("---\n\n")
    
    for release in releases:
        tag_name = release["tag_name"]
        release_name = release["name"]
        published_at = release["published_at"]
        html_url = release["html_url"]
        body = release.get("body", "")
        
        lines.append(f"## {release_name}\n\n")
        lines.append(f"- **Tag:** `{tag_name}`\n")
        lines.append(f"- **Published:** {published_at}\n")
        lines.append(f"- **URL:** {html_url}\n\n")
        
        if body:
            lines.append(f"### Release Notes\n\n")
            lines.append(f"{body}\n\n")
        
        lines.append("---\n\n")
    
    with open(output_file, 'w') as f:
        f.writelines(lines)
    
    print(f"✓ Report generated: {output_file}")
    print(f"  Total releases: {len(releases)}")


if __name__ == "__main__":
    # Release data from GitHub MCP
    releases_json = """[{"tag_name":"v5.16.0","name":"v5.16.0-20251006","published_at":"2025-10-06T21:36:35Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.16.0","body":"Release v5.16.0","draft":false},{"tag_name":"v5.15.0","name":"v5.15.0-20250902","published_at":"2025-09-02T20:22:20Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.15.0","body":"Release v5.15.0","draft":false},{"tag_name":"v5.14.1","name":"v5.14.1-20250818","published_at":"2025-08-18T17:42:41Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.14.1","body":"Release v5.14.1","draft":false},{"tag_name":"v5.14.0","name":"v5.14.0-20250804","published_at":"2025-08-04T18:33:55Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.14.0","body":"Release v5.14.0","draft":false},{"tag_name":"v5.13.0","name":"v5.13.0-20250625","published_at":"2025-06-25T20:53:30Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.13.0","body":"Release v5.13.0","draft":false},{"tag_name":"v5.12.0","name":"v5.12.0-20250512","published_at":"2025-05-12T22:12:02Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.12.0","body":"Release v5.12.0","draft":false},{"tag_name":"v5.11.1","name":"v5.11.1-20250415","published_at":"2025-04-15T19:31:46Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.11.1","body":"Release v5.11.1","draft":false},{"tag_name":"v5.10.1","name":"v5.10.1-20250415","published_at":"2025-04-15T17:42:29Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.10.1","body":"Release v5.10.1","draft":false},{"tag_name":"v5.11.0","name":"v5.11.0-20250409","published_at":"2025-04-09T23:15:50Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.11.0","body":"Release v5.11.0","draft":false},{"tag_name":"v5.10.0","name":"v5.10.0-20250312","published_at":"2025-03-12T15:49:50Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.10.0","body":"Release v5.10.0","draft":false},{"tag_name":"v5.9.0","name":"v5.9.0-20250205","published_at":"2025-02-05T17:52:29Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.9.0","body":"Release v5.9.0","draft":false},{"tag_name":"v5.8.0","name":"v5.8.0-20241205","published_at":"2024-12-05T22:18:25Z","html_url":"https://github.com/rundeck/rundeck/releases/tag/v5.8.0","body":"Release v5.8.0","draft":false}]"""
    
    releases = json.loads(releases_json)
    
    start_date = "2024-11-01"
    end_date = "2025-10-03"
    output_file = "deployed_prs_report.md"
    
    filtered_releases = filter_releases_by_date(releases, start_date, end_date)
    generate_report(filtered_releases, start_date, end_date, output_file)
